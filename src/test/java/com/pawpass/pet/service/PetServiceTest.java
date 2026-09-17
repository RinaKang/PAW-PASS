package com.pawpass.pet.service;

import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.domain.PetSize;
import com.pawpass.pet.dto.PetRequest;
import com.pawpass.pet.dto.PetResponse;
import com.pawpass.pet.repository.PetRepository;
import com.pawpass.user.domain.User;
import com.pawpass.user.repository.UserRepository;
import com.pawpass.user.service.ProfileImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 대표 반려동물(2026-09-13) 자동 지정/해제 로직 위주로 검증한다.
 * Pet.id는 @GeneratedValue(IDENTITY)라 실제 저장 없이는 채워지지 않으므로, 테스트에서만 리플렉션으로
 * 직접 세팅한다(운영 코드에 테스트용 세터를 넣지 않기 위함).
 */
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProfileImageStorage profileImageStorage;

    private PetService petService;

    private void init() {
        petService = new PetService(petRepository, userRepository, profileImageStorage);
    }

    @Test
    void 첫_반려동물_등록시_자동으로_대표가_된다() {
        init();
        Pet saved = pet(10L);
        when(petRepository.save(any())).thenReturn(saved);
        when(petRepository.findAllByUserId(1L)).thenReturn(List.of(saved)); // 이번이 유일한 반려동물
        User user = user();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PetResponse result = petService.create(1L, petRequest());

        assertThat(result.isPrimary()).isTrue();
        assertThat(user.getPrimaryPetId()).isEqualTo(10L);
    }

    // 2026-09-13: 반려동물 등록 폼 "동반 시 구비 가능한 용품" 다중선택 확장(입마개/배변봉투/유모차·웨건/기저귀·매너벨트).
    @Test
    void 등록시_구비용품_필드가_전부_반영된다() {
        init();
        PetRequest request = new PetRequest("초코", "강아지", "말티즈", 3.0, PetSize.SMALL,
                true, false, true, false, true, false);
        when(petRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(petRepository.findAllByUserId(1L)).thenReturn(List.of(pet(10L)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));

        PetResponse result = petService.create(1L, request);

        assertThat(result.hasCarrier()).isTrue();
        assertThat(result.hasLeash()).isFalse();
        assertThat(result.hasMuzzle()).isTrue();
        assertThat(result.hasWasteBags()).isFalse();
        assertThat(result.hasStroller()).isTrue();
        assertThat(result.hasDiaper()).isFalse();
    }

    @Test
    void 두번째_반려동물은_자동으로_대표가_되지_않는다() {
        init();
        Pet existing = pet(10L);
        Pet saved = pet(20L);
        when(petRepository.save(any())).thenReturn(saved);
        when(petRepository.findAllByUserId(1L)).thenReturn(List.of(existing, saved)); // 이미 1마리 있었음

        PetResponse result = petService.create(1L, petRequest());

        assertThat(result.isPrimary()).isFalse();
        verify(userRepository, never()).findById(any()); // 자동 지정 로직 자체를 안 탐 - User 조회 불필요
    }

    @Test
    void 목록_조회시_대표_반려동물만_isPrimary_true다() {
        init();
        User user = user();
        user.changePrimaryPet(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findAllByUserId(1L)).thenReturn(List.of(pet(10L), pet(20L)));

        List<PetResponse> result = petService.findAllByUser(1L);

        assertThat(result).extracting(PetResponse::id, PetResponse::isPrimary)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, true),
                        org.assertj.core.groups.Tuple.tuple(20L, false)
                );
    }

    @Test
    void 대표_반려동물_삭제시_남은_게_1마리면_그걸로_자동_재지정된다() {
        init();
        User user = user();
        user.changePrimaryPet(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(pet(10L)));
        when(petRepository.findAllByUserId(1L)).thenReturn(List.of(pet(20L))); // 삭제 후 남은 것

        petService.delete(1L, 10L);

        assertThat(user.getPrimaryPetId()).isEqualTo(20L);
    }

    @Test
    void 대표_반려동물_삭제시_남은_게_2마리_이상이면_대표_지정이_해제된다() {
        init();
        User user = user();
        user.changePrimaryPet(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(pet(10L)));
        when(petRepository.findAllByUserId(1L)).thenReturn(List.of(pet(20L), pet(30L)));

        petService.delete(1L, 10L);

        assertThat(user.getPrimaryPetId()).isNull();
    }

    @Test
    void 대표가_아닌_반려동물_삭제는_대표_지정에_영향_없다() {
        init();
        User user = user();
        user.changePrimaryPet(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(petRepository.findByIdAndUserId(20L, 1L)).thenReturn(Optional.of(pet(20L)));

        petService.delete(1L, 20L);

        assertThat(user.getPrimaryPetId()).isEqualTo(10L);
        verify(petRepository, never()).findAllByUserId(any()); // 대표가 아니었으니 재지정 로직 자체를 안 탐
    }

    // 2026-09-17: 반려동물 프로필 이미지 업로드 추가 - UserService.updateProfileImage와 같은 흐름
    // (소유권 확인 후 저장, Pet.imageUrl 갱신, 예전 파일 정리)을 대상만 Pet으로 바꿔서 탄다.
    @Test
    void 프로필_이미지를_업로드하면_imageUrl이_새_URL로_바뀐다() {
        init();
        Pet pet = pet(10L);
        when(petRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(profileImageStorage.store(image, "pet-images", 10L))
                .thenReturn("http://localhost:8080/uploads/pet-images/10_new.jpg");

        PetResponse result = petService.updateProfileImage(1L, 10L, image);

        assertThat(result.imageUrl()).isEqualTo("http://localhost:8080/uploads/pet-images/10_new.jpg");
        assertThat(pet.getImageUrl()).isEqualTo("http://localhost:8080/uploads/pet-images/10_new.jpg");
    }

    @Test
    void 프로필_이미지_업로드_시_예전_파일을_정리한다() {
        init();
        Pet pet = pet(10L);
        pet.updateImage("http://localhost:8080/uploads/pet-images/10_old.jpg");
        when(petRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user()));
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1});
        when(profileImageStorage.store(any(), eq("pet-images"), eq(10L)))
                .thenReturn("http://localhost:8080/uploads/pet-images/10_new.jpg");

        petService.updateProfileImage(1L, 10L, image);

        verify(profileImageStorage).deleteIfManaged("http://localhost:8080/uploads/pet-images/10_old.jpg", "pet-images");
    }

    @Test
    void 본인_소유가_아닌_반려동물은_프로필_이미지를_업로드할_수_없다() {
        init();
        when(petRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> petService.updateProfileImage(1L, 99L, image))
                .isInstanceOf(IllegalArgumentException.class);
        verify(profileImageStorage, never()).store(any(), any(), any());
    }

    private PetRequest petRequest() {
        return new PetRequest("초코", "강아지", "말티즈", 3.0, PetSize.SMALL, true, true, false, false, false, false);
    }

    private User user() {
        return User.builder().googleId("g").email("e@e.com").name("사용자").picture(null).build();
    }

    private Pet pet(long id) {
        Pet pet = Pet.builder()
                .userId(1L).name("초코").species("강아지").breed("말티즈").weight(3.0).size(PetSize.SMALL)
                .hasCarrier(true).hasLeash(true).build();
        setId(pet, id);
        return pet;
    }

    private void setId(Pet pet, long id) {
        try {
            Field field = Pet.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(pet, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
