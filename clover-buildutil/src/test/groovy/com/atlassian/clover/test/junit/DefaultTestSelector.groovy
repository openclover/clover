package com.atlassian.clover.test.junit

@Singleton
public class DefaultTestSelector {
  public Closure closure = { it.name =~ /^test*/ }
}