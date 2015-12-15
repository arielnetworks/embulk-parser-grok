# Grok parser plugin for Embulk

Embulk parser plugin using [grok](https://github.com/thekrakken/java-grok)

## Overview

* **Plugin type**: parser
* **Guess supported**: yes

## Configuration

### for run

- **grok_pattern**: A grok pattern name that match with log record. (string, required)
- **first_line_pattern**: A grok pattern name that match with the first line of log record, when log record is multi line. (string, default: null)
- **grok_pattern_files**: Any grok pattern file's paths. (string[], required)
- **timestamp_parser**: (string, default: ruby)
  - ruby: Use JRuby's timestamp parser.
  - SimpleDateFormat: Use Java's SimpleDateFormat
  - epoch: Milliseconds since the epoch
- **stop_on_invalid_record**: Stop bulk load transaction if a file includes invalid record (boolean, default: false)

### for guess

- **guess_patterns**: Any pattern names for guessing log format. (string[], required)

## Example

### parse apache log file

```yaml
in:
  type: file
  path_prefix: src/test/resources/apache.log
  parser:
    type: grok
    grok_pattern_files:
      - pattern/grok-patterns
      - pattern/my-patterns
    timestamp_parser: ruby
    grok_pattern: '%{COMBINEDAPACHELOG}'
    stop_on_invalid_record: false
    charset: UTF-8
    newline: CRLF
    columns:
    - {name: request, type: string}
    - {name: agent, type: string}
    - {name: COMMONAPACHELOG, type: string}
    - {name: auth, type: string}
    - {name: ident, type: string}
    - {name: verb, type: string}
    - {name: referrer, type: string}
    - {name: bytes, type: long}
    - {name: response, type: long}
    - {name: clientip, type: string}
    - {name: COMBINEDAPACHELOG, type: string}
    - {name: httpversion, type: string}
    - {name: rawrequest, type: string}
    - {name: timestamp, format: '%d/%b/%Y:%T %z', type: timestamp}
```

### parse multiline file

```yaml
in:
  type: file
  path_prefix: src/test/resources/multiline.log
  parser:
    type: grok
    grok_pattern_files:
      - pattern/grok-patterns
      - pattern/my-patterns
    timestamp_parser: ruby
    first_line_pattern: '%{MULTILINELOG_FIRSTLINE}'
    grok_pattern: '%{MULTILINELOG}'
    charset: UTF-8
    newline: CRLF
    columns:
    - {name: timestamp, format: '%Y-%m-%d %H:%M:%S.%N %z', type: timestamp}
    - {name: log_level, type: string}
    - {name: message, type: string}
    - {name: stack_trace, type: string}
```

### guess

```yaml
in:
  type: file
  path_prefix: src/test/resources/apache.log
  parser:
    charset: UTF-8
    newline: CRLF
    type: grok
    grok_pattern_files:
      - pattern/grok-patterns
      - pattern/my-patterns
    guess_patterns:
      - "%{COMBINEDAPACHELOG}"
      - "%{COMMONAPACHELOG}"
    timestamp_parser: ruby
```

```
$ embulk install embulk-parser-grok
$ embulk guess -g grok config.yml -o guessed.yml
```

## Build

```
$ ./gradlew gem
```
