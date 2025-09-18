#!/usr/bin/env bash
set -xeuo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
ROOT_DIR="$SCRIPT_DIR/../.."

HELP_MD="$ROOT_DIR/help.md"

echo "Print help guide"

cat > $HELP_MD <<- "MESSAGE"
## ℹ️ Systemtests Help ℹ️
You can run system tests with the following parameters:
* `--testcase=<name>` (optional): Run a specific test case. Use Maven style, e.g. [TopicST | KafkaST#testKafkaTwo...]
* `--profile=<profile>` (optional): Profile to use, e.g. [regression]
* `--env=<VAR1=val1;VAR2=val2>` (optional): Set environment variables for the test
* `--install-type=<olm|yaml>` (optional): Choose installation type. Default is [olm]
* `--retry-count=<number>` (optional): Number of Maven retries. Default is [0]
* `--help`: Show this help message

Note: Use either `--testcase` or `--profile` to select tests to run, setting both will result in
github actions prioritizing testcase over profile. If neither is provided, no systemtest will start.

### Example usage:
```bash
/systemtests run --profile=regression --env=MY_ENV=one;SECOND=two
/systemtests run --testcase=TopicST --install-type=olm --retry-count=3
```
MESSAGE

# Comment help
gh pr comment $PR_NUMBER --repo $REPO --body-file $HELP_MD

echo "Stop further workflow execution. Exiting workflow"
gh run cancel $RUN_ID --repo $REPO