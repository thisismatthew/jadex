@REM perform a clean distribution build
gradlew --profile -Pdist=publishdists clean test distZips -x javadoc -x lint -x generatereleaseJavadoc -x lintVitalRelease
@rem  --parallel --continue