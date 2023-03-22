package com.atlassian.clover.test.junit

@Singleton
class DefaultTestSelector {
  public Closure closure = { it.name =~ /^test*/ }
}