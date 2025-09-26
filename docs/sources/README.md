# StreamsHub Console Documentation

The documentation is written in [Asciidoc](https://asciidoc.org/) and can be built locally using [asciidoctor](https://asciidoctor.org/):

```shell
asciidoctor index.adoc
```

This will build the docs as single page (`index.html`).

The docs are published via the main StreamsHub site [repository](https://github.com/streamshub/streamshub-site) and can be viewed on the main site at: [https://www.streamshub.io/](https://www.streamshub.io/).
All released versions (via git specified git tags), as well as the current `main` branch, of the docs will be published.

## Conventions

Each documentation file requires the following:

 - Content type
 - Anchor ID
 - Abstract tag

**Content Type** 

Defined at the top of the file:
- :_mod-docs-content-type: ASSEMBLY 		
- :_mod-docs-content-type: PROCEDURE 
- :_mod-docs-content-type: CONCEPT
- :_mod-docs-content-type: REFERENCE 
- :_mod-docs-content-type: SNIPPET

**Anchor ID**

Use the same name as the file (with dashes) plus a `_{context}` variable: `[id='name-of-file_{context}']`

The context variable is defined in the assembly of a guide, such as `:context: console`. Context variables allow reuse of the same content. Anchor IDs allow cross-referencing. You can also add anchors to subheadings.

**Abstract**

Start each file with an introductory paragraph. Mark it by adding a `[role="_abstract"]` tag above it.
