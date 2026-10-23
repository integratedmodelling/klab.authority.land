package org.integratedmodelling.klab.authority.land;

import java.math.BigDecimal;
import java.math.BigInteger;

/** Small exact arithmetic kernel for bounded alpha cuts; no floating-point decision boundaries. */
record ExactFraction(BigInteger numerator, BigInteger denominator)
    implements Comparable<ExactFraction> {
  static final ExactFraction ZERO = of(0), ONE = of(1), TWO = of(2);

  ExactFraction {
    if (denominator.signum() == 0) throw new ArithmeticException("Zero denominator");
    if (denominator.signum() < 0) {
      numerator = numerator.negate();
      denominator = denominator.negate();
    }
    BigInteger gcd = numerator.gcd(denominator);
    numerator = numerator.divide(gcd);
    denominator = denominator.divide(gcd);
  }

  static ExactFraction of(long value) {
    return new ExactFraction(BigInteger.valueOf(value), BigInteger.ONE);
  }

  static ExactFraction of(BigDecimal value) {
    return value.scale() >= 0
        ? new ExactFraction(value.unscaledValue(), BigInteger.TEN.pow(value.scale()))
        : new ExactFraction(
            value.unscaledValue().multiply(BigInteger.TEN.pow(-value.scale())), BigInteger.ONE);
  }

  ExactFraction add(ExactFraction other) {
    return new ExactFraction(
        numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)),
        denominator.multiply(other.denominator));
  }

  ExactFraction subtract(ExactFraction other) {
    return add(new ExactFraction(other.numerator.negate(), other.denominator));
  }

  ExactFraction multiply(ExactFraction other) {
    return new ExactFraction(
        numerator.multiply(other.numerator), denominator.multiply(other.denominator));
  }

  ExactFraction divide(ExactFraction other) {
    return new ExactFraction(
        numerator.multiply(other.denominator), denominator.multiply(other.numerator));
  }

  @Override
  public int compareTo(ExactFraction other) {
    return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator));
  }

  /** Only used for dyadic bisection endpoints, whose decimal expansions terminate. */
  BigDecimal decimal() {
    return new BigDecimal(numerator).divide(new BigDecimal(denominator));
  }
}
