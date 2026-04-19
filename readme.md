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
jbang jsit@yavuztas ./
```

Serve a single file with a custom base:
```bash
jbang jsit@yavuztas ./test.pdf /mydoc
```

Custom port:
```bash
jbang jsit@yavuztas ./ /docs 8888
```

Then open:
```
http://localhost:8080/
http://localhost:8080/mydoc
http://localhost:8888/docs
```

## Native CLI Experience (install)

Install as a local command:
```bash
jbang app install --name jsit jsit@yavuztas
```

Now use it like a regular CLI:
```bash
jsit ./
jsit ./file.pdf /myfile
jsit ./ /docs
jsit ./ /docs 8888
```

## Advanced: Native Binary

You can compile to a native executable using GraalVM:
```bash
jbang build --native --build-dir=./ jsit@yavuztas
```

Run:
```bash
./Jsit.bin ./ /docs
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
