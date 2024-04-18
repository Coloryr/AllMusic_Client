mkdir "build"
mkdir ".gradle"

$array = @(
    "fabric_1_14_4", "fabric_1_52_2", "fabric_1_16_5", "fabric_1_17_1", "fabric_1_18_2", 
    "forge_1_19", "fabric_1_19_3", "fabric_1_19_4", "fabric_1_20",
    "forge_1_7_10", "forge_1_12_2", "forge_1_14_4", "forge_1_15_2", "forge_1_16_5", 
    "forge_1_17_1", "forge_1_18_2", "forge_1_19_2", "forge_1_19_3", "forge_1_20", 
    "forge_1_20_2"
)

foreach ($item in $array) {
    mkdir "$item/src/main/java/coloryr/allmusic_client"
    mkdir "$item/src/main/resources/coloryr/allmusic_client/player/decoder"
    mklink /j "$item/src/main/resources/coloryr/allmusic_client/player/decoder/mp3" "mp3"
    mklink /j "$item/src/main/java/coloryr/allmusic_client/player" "player"
    mklink /j "$item/src/main/java/coloryr/allmusic_client/hud" "hud"
    mklink /j "$item/build" "build"
    mklink /j "$item/.gradle" ".gradle"
}