# What's this

This module contains third party libraries taken out from Intellij IDEA installations. They are being used
for unit testing. Keep here the latest patch version of every IDEA product line supported.

# Maintenance

Since IDEA12 the Apache Commons Collections (commons-collections.jar) is not included in the IDEA/lib directory.
Therefore, we're packaging Collections library into clover-idea.jar using a repackaged version ('clover.' namespace
prefix added) of clover-commons-collections.jar.

# References

See JetBrains page [http://confluence.jetbrains.net/display/IDEADEV/Building+Plugins+with+IntelliJ+IDEA+7.0+and+Later]
