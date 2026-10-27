# JWT: Extra Claims & Signing Key

*Quick study notes • Spring Boot / Java*

## 1. What are `extraClaims`?

`extraClaims` is a map of additional information that you want to put
inside a JWT payload. The email identifies the token's subject; extra
claims can carry details such as a role, user ID, or department.

``` java
Map<String, Object> extraClaims = new HashMap<>();
extraClaims.put("role", "ADMIN");
extraClaims.put("userId", 101);
extraClaims.put("department", "IT");
```

### Example payload

``` json
{
  "sub": "shivansh@gmail.com",
  "role": "ADMIN",
  "userId": 101,
  "department": "IT"
}
```

**Remember:** `Map<String, Object>` means the keys are strings, while
values can be different Java types, such as `String`, `Integer`,
`Boolean`, or lists.

## 2. How `extraClaims` gets into the token

A typical JJWT builder can add the map to the token and set the email as
the subject:

``` java
return Jwts.builder()
    .claims(extraClaims)
    .subject(email)
    .issuedAt(new Date())
    .expiration(expiryDate)
    .signWith(getSigningKey())
    .compact();
```

**Note:** The exact builder methods depend on the JJWT version used in
the project.

## 3. What does `getSigningKey()` do?

This method converts the configured secret string into a cryptographic
key for HMAC-based JWT signing.

``` java
private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(
        secret.getBytes(StandardCharsets.UTF_8)
    );
}
```

  ------------------------------------------------------------------------
  Code                                 Meaning
  ------------------------------------ -----------------------------------
  `secret`                             The configured secret value, kept
                                       private by the backend.

  `getBytes(StandardCharsets.UTF_8)`   Converts the Java `String` into
                                       bytes, which cryptographic APIs
                                       use.

  `Keys.hmacShaKeyFor(...)`            Creates a `SecretKey` for HMAC
                                       signing and checks that the key is
                                       long enough for supported
                                       algorithms.

  `return`                             Returns the key so the JWT builder
                                       or verifier can use it.
  ------------------------------------------------------------------------

## 4. How the pieces fit together

1.  Build the JWT payload with the subject and any extra claims.
2.  Get the signing key from the configured secret.
3.  Sign the token using that key.
4.  Later, verify the signature with the matching key before trusting
    the token.

The signing key does not normally change on each call. The same
configured secret produces the same key, so the application can verify
tokens that it previously signed.

## 5. Security points to remember

-   Use a long, randomly generated secret with enough bytes for the
    selected HMAC algorithm (for HS256, at least 256 bits / 32 bytes).
-   Keep the secret in an environment variable or secrets manager; do
    not commit it to GitHub.
-   A JWT payload is usually encoded, not encrypted. Do not put
    passwords, API keys, or other secrets in claims.
-   Verify the JWT signature and validate expiration before trusting
    claims such as role or user ID.
-   `Keys.hmacShaKeyFor` does not make a weak password secure by
    stretching it; supply a properly generated key.

**One-line summary:** `extraClaims` adds custom data to the JWT payload;
`getSigningKey()` turns your configured secret into the key used to sign
and verify the token.

**Study tip:** If asked in an interview, explain the purpose first, then
walk through the code line by line.
