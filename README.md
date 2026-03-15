# FDLog Swarm

![Build](https://github.com/dicklieber/fdlog_full/actions/workflows/ci.yaml/badge.svg)
![Tests](https://img.shields.io/badge/tests-passed-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen)
![Release](https://img.shields.io/github/v/release/dicklieber/fdlog_full)
![License](https://img.shields.io/github/license/dicklieber/fdlog_full)

This project uses [Mill](https://mill-build.com/) as the build tool.

## Build and Development Commands

### Cleaning
To clean the project build artifacts:
```bash
./mill clean
```

### Running Tests
To run the project tests:
```bash
./mill fdswarm.test.testLocal
```

### Test Coverage Reports
This project uses Scoverage for code coverage.

To generate an HTML coverage report:
```bash
./mill fdswarm.scoverage.htmlReport
```
The report will be available at: `out/fdswarm/scoverage/htmlReport.dest/index.html`

## Logging
For information on how to configure and change log levels, see [docs/logging.md](docs/logging.md).

Other coverage report formats:
- **XML Report**: `./mill fdswarm.scoverage.xmlReport`
- **Console Report**: `./mill fdswarm.scoverage.consoleReport`
- **Cobertura XML**: `./mill fdswarm.scoverage.xmlCoberturaReport`
