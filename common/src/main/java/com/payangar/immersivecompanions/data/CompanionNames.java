package com.payangar.immersivecompanions.data;

import com.payangar.immersivecompanions.entity.CompanionGender;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.List;

/**
 * Handles generation of random names for companions.
 */
public class CompanionNames {

    private static final List<String> MALE_FIRST_NAMES = List.of(
            "Aldric", "Bjorn", "Cedric", "Duncan", "Edmund",
            "Finn", "Gareth", "Harald", "Ivan", "Jasper",
            "Klaus", "Leif", "Magnus", "Nils", "Osric",
            "Piotr", "Quinn", "Roland", "Stefan", "Theron"
    );

    private static final List<String> FEMALE_FIRST_NAMES = List.of(
            "Astrid", "Brynhild", "Celia", "Dagny", "Elena",
            "Freya", "Greta", "Helena", "Ingrid", "Johanna",
            "Katya", "Liora", "Mira", "Nadia", "Olga",
            "Petra", "Quinn", "Rosalind", "Sigrid", "Thora"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Ashford", "Blackwood", "Carver", "Dunmore", "Everhart",
            "Fairwind", "Graystone", "Holloway", "Ironside", "Jansen",
            "Kestrel", "Lightfoot", "Moorland", "Northbrook", "Oakenshield",
            "Pinewood", "Quicksilver", "Ravencrest", "Stormwind", "Thornwood",
            "Underhill", "Valdris", "Whitmore", "Yarrow", "Zimmer",
            "Ashborne", "Brightwater", "Coldwell", "Duskwood", "Elderwood",
            "Frostborne", "Greenfield", "Highgate", "Ironfist", "Kingsley"
    );

    /**
     * Generates a random name based on the companion's gender.
     *
     * @param gender The gender of the companion
     * @param random The random source to use
     * @return A Component containing the full name
     */
    public static Component generateName(CompanionGender gender, RandomSource random) {
        List<String> firstNames = gender == CompanionGender.MALE ? MALE_FIRST_NAMES : FEMALE_FIRST_NAMES;

        String firstName = firstNames.get(random.nextInt(firstNames.size()));
        String lastName = LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));

        return Component.literal(firstName + " " + lastName);
    }
}
