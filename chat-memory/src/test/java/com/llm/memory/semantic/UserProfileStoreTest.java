package com.llm.memory.semantic;

import com.llm.dto.UserProfile;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserProfileStoreTest {

    // a fresh in-memory database per test instance
    private final UserProfileStore store = new UserProfileStore(JdbcClient.create(
            new DriverManagerDataSource("jdbc:hsqldb:mem:profile-" + UUID.randomUUID(), "sa", "")));

    @Test
    void anUnknownPersonHasNoProfile() {
        assertThat(store.find("dilip")).isEmpty();
    }

    @Test
    void theFirstFactCreatesTheProfile() {
        store.update("dilip", ProfileField.VEHICLE, "Honda Civic");

        assertThat(store.find("dilip")).contains(new UserProfile(null, "Honda Civic", null, null));
    }

    @Test
    void aNewValueReplacesTheOldOneAndKeepsTheOtherFacts() {
        store.update("dilip", ProfileField.VEHICLE, "Tesla Model 3");
        store.update("dilip", ProfileField.CONTACT_PREFERENCE, "Text only, never call");

        store.update("dilip", ProfileField.VEHICLE, "Honda Civic");

        assertThat(store.find("dilip"))
                .contains(new UserProfile(null, "Honda Civic", null, "Text only, never call"));
    }

    @Test
    void profilesBelongToOnePerson() {
        store.update("dilip", ProfileField.HOME_BRANCH, "Plano");

        store.delete("dilip");
        store.update("alex", ProfileField.HOME_BRANCH, "Frisco");

        assertThat(store.find("dilip")).isEmpty();
        assertThat(store.find("alex")).contains(new UserProfile(null, null, "Frisco", null));
    }
}
