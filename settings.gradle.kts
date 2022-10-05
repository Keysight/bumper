if(JavaVersion.current() != JavaVersion.VERSION_11){
    throw GradleException("This build must be run with java 11")
}

rootProject.name = "bumper"
