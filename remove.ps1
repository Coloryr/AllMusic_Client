$array = @(
    "fabric_1_14_4", "fabric_1_52_2", "fabric_1_16_5", "fabric_1_17_1", "fabric_1_18_2", 
    "forge_1_19", "fabric_1_19_3", "fabric_1_19_4", "fabric_1_20",
    "forge_1_7_10", "forge_1_12_2", "forge_1_14_4", "forge_1_15_2", "forge_1_16_5", 
    "forge_1_17_1", "forge_1_18_2", "forge_1_19_2", "forge_1_19_3", "forge_1_20", 
    "forge_1_20_2"
)

foreach ($item in $array) {
    rmdir "$item/src/main/resources/coloryr/allmusic_client/player/decoder/mp3"
    rmdir "$item/src/main/java/coloryr/allmusic_client/player"
    rmdir "$item/src/main/java/coloryr/allmusic_client/hud"
    rmdir /s /q  "$item/build"
    rmdir /s /q  "$item/.gradle"
}