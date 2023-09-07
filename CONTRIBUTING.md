## Contributing to the project

**Want to contribute? Great!**
We try to make it easy, and all contributions, even the smaller ones, are more than welcome.
This includes bug reports, fixes, documentation, examples...
But first, read this page (including the small print at the end).

## Legal

All original contributions to kafka-admin-api are licensed under the
[ASL - Apache License](https://www.apache.org/licenses/LICENSE-2.0),
version 2.0 or later, or, if another license is specified as governing the file or directory being
modified, such other license.

All contributions are subject to the [Developer Certificate of Origin (DCO)](https://developercertificate.org/).
The DCO text is also included verbatim in the [dco.txt](dco.txt) file in the root directory of the repository.

## Reporting an issue

This project uses GitHub issues to manage the issues. Open an issue directly in GitHub. you can also open JIRA issues at https://issues.redhat.com/browse/MGDSTRM

If you believe you found a bug, and it's likely possible, please indicate a way to reproduce it, what you are seeing and what you would expect to see.

## Before you contribute

To contribute, use GitHub Pull Requests, from your **own** fork.

Also, make sure you have set up your Git authorship correctly:

```
git config --global user.name "Your Full Name"
git config --global user.email your.email@example.com
```

If you use different computers to contribute, please make sure the name is the same on all your computers.

We use this information to acknowledge your contributions in release announcements.

### Code reviews

All submissions, including submissions by project members, need to be reviewed by at least two kafka-admin-api committers before being merged.

[GitHub Pull Request Review Process](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/reviewing-changes-in-pull-requests/about-pull-request-reviews) is followed for every pull request.

### Coding Guidelines

 * We decided to disallow `@author` tags in the Javadoc: they are hard to maintain
 * Please properly squash your pull requests before submitting them. Fixup commits can be used temporarily during the review process but things should be squashed at the end to have meaningful commits.

### Continuous Integration

 kafka-admin-api CI is based on GitHub Actions, which means that everyone has the ability to automatically execute CI in their forks as part of the process of making changes. We ask that all non-trivial changes go through this process, so that the contributor gets immediate feedback, while at the same time keeping our CI fast and healthy for everyone.

### Tests and documentation are not optional

Don't forget to include tests in your pull requests.
Also don't forget the documentation (reference documentation, javadoc...).

### Installing Checkstyle

Project uses checkstyle mvn plugin that is executed during `mvn validate` pase.
Please follow your ide setup for checkstyle. For example for intelij:

https://plugins.jetbrains.com/plugin/1065-checkstyle-idea

## Regenerating OpenAPI file

PRs that make changes in the API should update openapi file by executing:

```
mvn -Popenapi-generate process-classes
```

Please commit generated files along with the PR for review.

### Interacting with local kafka

1. Creating topic

```
kafka-topics.sh --create --bootstrap-server localhost:9092  --partitions=3 --replication-factor=1 --topic test --command-config ./hack/binscripts.properties
```

2. Produce messages using kcat
```
kcat -b localhost:9092 -F ./hack/kcat.properties -P -t test
```


4. Consume messages
```
 kcat -b localhost:9092 -F ./hack/kcat.properties  -C -t test
```

6. Interact with the API to view results
`
curl -s -u admin:admin-secret http://localhost:8080/api/v1/consumer-groups | jq
`

