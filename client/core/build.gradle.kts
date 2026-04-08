val shadowImplementation by configurations.getting

dependencies {
    shadowImplementation("org.apache.httpcomponents.client5:httpclient5:5.6")
    shadowImplementation("org.apache.httpcomponents.core5:httpcore5:5.4")
    shadowImplementation("org.apache.httpcomponents.core5:httpcore5-h2:5.4")

    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.lwjgl.lwjgl:lwjgl:2.9.3")
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
}
