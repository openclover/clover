package org.openclover.buildutil.test.junit

@Singleton
class DefaultTestSelector {
  public Closure closure = { it.name =~ /^test*/ }
}