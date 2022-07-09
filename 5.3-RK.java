package chapter5.section3;

import chapter1.section3.Queue;
import edu.princeton.cs.algs4.StdOut;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by Rene Argento on 24/02/18.
 */
// Monte Carlo version
// Runs in O(N + M)
// Extra space: 1
// Does not require backup in the input text
// Has a probabilistic guarantee of giving the correct output

// Las Vegas version
// Typical running time is O(N + M) -> Has a probabilistic guarantee of running in this time.
// Worst case is O(N * M)
// Extra space: 1
// Requires backup in the input text
// Always gives the correct output
public class RabinKarp implements SubstringSearch {

  // 模式字符串
  protected String pattern;        // Only needed in the Las Vegas version
  // 模式字符串散列值
  protected long patternHash;
  // 模式字符串长度
  protected int patternLength;
  // 一个很大的素数
  protected long largePrimeNumber; // a large prime, small enough to avoid long overflow
  // 字母表的大小
  protected int alphabetSize = 256;
  // R^(M-1) % Q
  protected long rm;               // rm = alphabetSize^(patternLength - 1) % largePrimeNumber
  private boolean isMonteCarloVersion;

  public RabinKarp(String pattern, boolean isMonteCarloVersion) {

    if (pattern == null) {
      throw new IllegalArgumentException("Invalid pattern");
    }

    // 保存模式字符串
    this.pattern = pattern;
    patternLength = pattern.length();
    this.isMonteCarloVersion = isMonteCarloVersion;

    largePrimeNumber = longRandomPrime();

    rm = 1;
    for (int patternIndex = 1; patternIndex <= patternLength - 1; patternIndex++) {
      // 计算R^(M-1) % Q  用于减去第一个数字时的计算
      rm = (rm * alphabetSize) % largePrimeNumber;  // Compute alphabetSize^(patternLength - 1) % largePrimeNumber
    }                                                 // for use in removing leading digit.

    patternHash = hash(pattern);
  }

  // A random 31-bit prime
  protected long longRandomPrime() {
    BigInteger prime = BigInteger.probablePrime(31, new Random());
    return prime.longValue();
  }

  protected boolean check(String text, int textIndex) {
    if (isMonteCarloVersion) {
      return true;
    }

    // Las Vegas version
    for (int patternIndex = 0; patternIndex < patternLength; patternIndex++) {
      if (pattern.charAt(patternIndex) != text.charAt(textIndex + patternIndex)) {
        return false;
      }
    }

    return true;
  }

  // Horner's method applied to modular hashing
  protected long hash(String key) {
    // Compute hash for key[0..patternLength - 1]
    long hash = 0;

    for (int patternIndex = 0; patternIndex < patternLength; patternIndex++) {
      hash = (hash * alphabetSize + key.charAt(patternIndex)) % largePrimeNumber;
    }

    return hash;
  }

  // Search for a hash match in the text.
  // Returns the index of the first occurrence of the pattern string in the text string or textLength if no such match.
  public int search(String text) {
    // 在正文中查找相等的散列值
    int textLength = text.length();

    if (textLength < patternLength) {
      return textLength;
    }

    // 先计算txt中前M个字符
    long textHash = hash(text);

    // 一开始就匹配成功，并挨个审查
    if (patternHash == textHash && check(text, 0)) {
      return 0;  // match
    }

    // i = M 
    // 这里假设eeeeabcd找abcd，上来eeee不对所以走循环
    // i = M 等于4，然后txthash = hash(eeee) - e + a就可以了（得到的是eeea） 所以我们从i等于M开始循环就可以
    for (int textIndex = patternLength; textIndex < textLength; textIndex++) {
      // largePrimeNumber Q
      // 减去第一个数字，加上最后一个数字，再次检查匹配
      // Remove leading character, add trailing character, check for match
      textHash = (textHash + largePrimeNumber - rm * text.charAt(textIndex - patternLength) % largePrimeNumber)
        % largePrimeNumber;                               
      textHash = (textHash * alphabetSize + text.charAt(textIndex)) % largePrimeNumber;

      // eeeeabcd，找abcd，i = 4, 等i到7的时候找的txthash就是abcd，这时候返回值为,7 - 4 + 1为4
      int offset = textIndex - patternLength + 1;

      // 找到匹配
      if (patternHash == textHash && check(text, offset)) {
        return offset;  // match
      }
    }

    // 未找到匹配
    return textLength;      // no match
  }

  // Count the occurrences of pattern in the text
  public int count(String text) {
    int count = 0;

    int occurrenceIndex = searchFromIndex(text, 0);

    while (occurrenceIndex != text.length()) {
      count++;

      if (occurrenceIndex + 1 >= text.length()) {
        break;
      }

      occurrenceIndex = searchFromIndex(text, occurrenceIndex + 1);
    }

    return count;
  }

  // Finds all the occurrences of pattern in the text
  public Iterable<Integer> findAll(String text) {
    Queue<Integer> offsets = new Queue<>();

    int occurrenceIndex = searchFromIndex(text, 0);

    while (occurrenceIndex != text.length()) {
      offsets.enqueue(occurrenceIndex);

      if (occurrenceIndex + 1 >= text.length()) {
        break;
      }

      occurrenceIndex = searchFromIndex(text, occurrenceIndex + 1);
    }

    return offsets;
  }

  // Searches for the pattern in the text starting at specified index
  protected int searchFromIndex(String text, int textStartIndex) {
    String eligibleText = text.substring(textStartIndex);

    int textLength = eligibleText.length();

    if (textLength < patternLength) {
      return textStartIndex + textLength;  // no match
    }

    long textHash = hash(eligibleText);

    if (patternHash == textHash && check(eligibleText, 0)) {
      return textStartIndex;  // match
    }

    for (int textIndex = patternLength; textIndex < textLength; textIndex++) {
      // Remove leading character, add trailing character, check for match
      textHash = (textHash + largePrimeNumber - rm * eligibleText.charAt(textIndex - patternLength) % largePrimeNumber)
              % largePrimeNumber;
      textHash = (textHash * alphabetSize + eligibleText.charAt(textIndex)) % largePrimeNumber;

      int offset = textIndex - patternLength + 1;

      if (patternHash == textHash && check(eligibleText, offset)) {
        return textStartIndex + offset;  // match
      }
    }

    return textStartIndex + textLength;      // no match
  }

    // Parameters example: AACAA AABRAACADABRAACAADABRA
    public static void main(String[] args) {
      String pattern = args[0];
      String text = args[1];

      RabinKarp rabinKarp = new RabinKarp(pattern, true);
      StdOut.println("text:    " + text);

      int offset = rabinKarp.search(text);
      StdOut.print("pattern: ");
      for (int i = 0; i < offset; i++) {
        StdOut.print(" ");
      }
      StdOut.println(pattern);
    }

}