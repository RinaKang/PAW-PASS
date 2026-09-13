package com.pawpass.user.service;

import com.pawpass.favorite.repository.FavoriteRepository;
import com.pawpass.pet.domain.Pet;
import com.pawpass.pet.domain.PetSize;
import com.pawpass.pet.repository.PetRepository;
import com.pawpass.trip.repository.TripRepository;
import com.pawpass.user.domain.User;
import com.pawpass.user.dto.PrimaryPetResponse;
import com.pawpass.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private UserService userService;

    private void init() {
        userService = new UserService(userRepository, petRepository, tripRepository, favoriteRepository);
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
}
