Running spock-example from Ant:

    ant clean
    ant report validate
    ant report-with-xml validate

Running spock-example from Gradle:

    gradle clean test cloverGenerateReport

Running spock-example from Maven:

    mvn clean clover:setup test clover:clover
