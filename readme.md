# JSIT - Just Serve It!

A minimal static file HTTP server. 

Single-file, JDK-only, zero dependencies, built for quick sharing and local testing.

## Features

- Serve files or directories over HTTP
- Zero dependencies (JDK only)
- Single-file distribution
- Works directly via JBang
- Optional native binary

## Requirements

- Java 17+
- [JBang](https://www.jbang.dev) installed

## Quick Start (no install)

Run directly from GitHub without installing anything:

```bash
jbang https://raw.githubusercontent.com/yavuztas/jsit/main/src/Jsit.java -- ./
```

Serve under a custom base path:

```bash
jbang https://raw.githubusercontent.com/yavuztas/jsit/main/jsit.java -- ./ 123
```

Custom port:

```bash
jbang https://raw.githubusercontent.com/yavuztas/jsit/main/jsit.java -- ./ 123 8888
```

Then open:

```
http://localhost:8080/
http://localhost:8080/123
http://localhost:8888/123
```

## Native CLI Experience (install)

Install as a local command:

```bash
jbang app install --name jsit https://raw.githubusercontent.com/yavuztas/jsit/main/jsit.java
```

Now use it like a regular CLI:

```bash
jsit ./
jsit ./file.pdf
jsit ./ 123
jsit ./ 123 8888
```

## Usage

### Serve directory
```bash
jsit ./
```

### Serve single file
```bash
jsit ./file.ext
```

### Serve under base path
```bash
jsit ./ 123
```

### Custom port
```bash
jsit ./ 123 8888
```

## Advanced: Native Binary

You can compile to a native executable using GraalVM:

```bash
jbang build --native jsit.java
```

Output:

```bash
./jsit
```

Run:

```bash
./jsit ./ 123
```

### Benefits

- Instant startup
- No JVM required at runtime
- Single portable binary

## Notes

- Uses JDK built-in HttpServer
- Directory listing is auto-generated
- Content-Type is best-effort detection
- Designed for local/dev usage (not production)

## License
Jsit is released under the [MIT License](https://github.com/yavuztas/jsit/blob/main/LICENSE).
