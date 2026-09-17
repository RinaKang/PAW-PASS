package com.pawpass.user.service;

import com.pawpass.favorite.repository.FavoriteRepository;
import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.domain.PetSize;
import com.pawpass.pet.repository.PetRepository;
import com.pawpass.trip.repository.TripRepository;
import com.pawpass.user.domain.User;
import com.pawpass.user.dto.PrimaryPetResponse;
import com.pawpass.user.dto.UserResponse;
import com.pawpass.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private ProfileImageStorage profileImageStorage;

    private UserService userService;

    private void init() {
        userService = new UserService(userRepository, petRepository, tripRepository, favoriteRepository, profileImageStorage);
    }

    @Test
    void 본인_소유_반려동물을_대표로_지정한다() {
        init();
        Pet pet = Pet.builder()
                .userId(1L).name("초코").species("강아지").breed("말티즈").weight(3.0).size(PetSize.SMALL)
                .hasCarrier(true).hasLeash(true).build();
        User user = User.builder().googleId("g").email("e@e.com").name("사용자").picture(null).build();
        when(petRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(pet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PrimaryPetResponse result = userService.updatePrimaryPet(1L, 10L);

        assertThat(result.primaryPetId()).isEqualTo(10L);
        assertThat(user.getPrimaryPetId()).isEqualTo(10L);
    }

    @Test
    void 본인_소유가_아닌_반려동물은_대표로_지정할_수_없다() {
        init();
        when(petRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updatePrimaryPet(1L, 99L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // 2026-09-15: 프로필 이미지 업로드 추가 - 저장은 ProfileImageStorage에 위임하고, User.picture를
    // 새 URL로 덮어쓴 뒤 예전 파일(우리가 저장한 것이었다면)을 정리한다.
    @Test
    void 프로필_이미지를_업로드하면_picture가_새_URL로_바뀐다() {
        init();
        User user = User.builder().googleId("g").email("e@e.com").name("사용자")
                .picture("http://localhost:8080/uploads/profile-images/old.jpg").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(profileImageStorage.store(image, "profile-images", 1L))
                .thenReturn("http://localhost:8080/uploads/profile-images/new.jpg");

        UserResponse result = userService.updateProfileImage(1L, image);

        assertThat(result.picture()).isEqualTo("http://localhost:8080/uploads/profile-images/new.jpg");
        assertThat(user.getPicture()).isEqualTo("http://localhost:8080/uploads/profile-images/new.jpg");
    }

    @Test
    void 프로필_이미지_업로드_시_예전_파일_정리를_새_파일_저장_이후에_시도한다() {
        init();
        User user = User.builder().googleId("g").email("e@e.com").name("사용자")
                .picture("https://lh3.googleusercontent.com/구글사진").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1});
        when(profileImageStorage.store(any(), eq("profile-images"), eq(1L)))
                .thenReturn("http://localhost:8080/uploads/profile-images/new.jpg");

        userService.updateProfileImage(1L, image);

        verify(profileImageStorage).deleteIfManaged("https://lh3.googleusercontent.com/구글사진", "profile-images");
    }

    @Test
    void 존재하지_않는_사용자는_프로필_이미지를_업로드할_수_없다() {
        init();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        MultipartFile image = new MockMultipartFile("image", "photo.jpg", "image/jpeg", new byte[]{1});

        assertThatThrownBy(() -> userService.updateProfileImage(99L, image))
                .isInstanceOf(IllegalArgumentException.class);
        verify(profileImageStorage, never()).store(any(), any(), any());
    }
}
