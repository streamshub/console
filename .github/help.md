## ℹ️ Systemtests Help ℹ️
You can run system tests with the following parameters:
* `--testcase=<name>` (optional): Run a specific test case. Use Maven style, e.g. [TopicST | KafkaST#testKafkaTwo...]
* `--profile=<profile>` (optional): Profile to use, e.g. [regression]
* `--env=<VAR1=val1;VAR2=val2>` (optional): Set environment variables for the test
* `--install-type=<olm|yaml>` (optional): Choose installation type. Default is [olm]
* `--retry-count=<number>` (optional): Number of Maven retries. Default is [0]
* `--help`: Show this help message

Note: Use either `--testcase` or `--profile` to select tests to run, setting both will result in
github actions prioretizing testcase over profile. If neither is provided, no systemtest will start.

### Example usage:
```bash
/systemtests run --profile=regression --env=MY_ENV=one;SECOND=two
/systemtests run --testcase=TopicST --install-type=olm --retry-count=3
```
