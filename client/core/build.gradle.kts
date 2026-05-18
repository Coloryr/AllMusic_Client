val shadowImplementation by configurations.getting

dependencies {
    shadowImplementation(project(":codec:buffercodec"))
    shadowImplementation(project(":codec"))

    shadowImplementation("org.apache.httpcomponents.client5:httpclient5:${Versions.httpclient5}")
    shadowImplementation("org.apache.httpcomponents.core5:httpcore5:${Versions.httpcore5}")
    shadowImplementation("org.apache.httpcomponents.core5:httpcore5-h2:${Versions.httpcore5_h2}")

    compileOnly("com.google.code.gson:gson:${Versions.gson}")
    compileOnly("org.lwjgl.lwjgl:lwjgl:2.9.3")
    compileOnly("org.apache.logging.log4j:log4j-core:2.25.4")
}
