# qtl-rso-annotation-pipeline

Creates RSO (Rat Strain Ontology) annotations on QTLs based on strain-QTL associations.

## Overview

Strains in RGD are linked to RSO terms via ontology synonyms (e.g. `RGD ID:12345`).
This pipeline propagates those RSO associations to QTLs that are linked to those strains,
creating QTL-RSO annotations with evidence code IEA and aspect S.

## Logic

1. **Safety check** — verifies no QTL-RSO annotations exist that were not created by
   this pipeline. If any are found, the pipeline aborts to prevent conflicts.
2. **Compute incoming annotations** — joins RSO term synonyms containing RGD IDs to
   strains, then to QTLs via strain-QTL associations, using reference RGD IDs from
   existing QTL annotations with aspect L.
3. **Load existing annotations** — retrieves all pipeline-created QTL-RSO annotations
   from the database.
4. **QC and sync** — compares incoming vs existing annotations by a composite key
   (ref, RGD ID, term, xref, qualifier, with-info, evidence). Inserts new, deletes
   obsolete, and leaves matching annotations unchanged.

## Logging

- `status` — pipeline progress and summary counts
- `insertedAnnots` / `deletedAnnots` — audit logs for each annotation change

## Configuration

Configured in `properties/AppConfigure.xml`:
- `createdBy` — the pipeline user account ID (181) used for annotation ownership

## Build and run

Requires Java 17. Built with Gradle:
```
./gradlew clean assembleDist
```