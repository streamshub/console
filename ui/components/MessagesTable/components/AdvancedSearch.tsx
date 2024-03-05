import { LimitSelector } from "@/components/MessagesTable/components/LimitSelector";
import {
  ActionGroup,
  Button,
  Form,
  FormGroup,
  FormHelperText,
  FormSection,
  Grid,
  GridItem,
  HelperText,
  HelperTextItem,
  Panel,
  PanelMain,
  PanelMainBody,
  Popper,
  SearchInput,
  TextInput,
} from "@patternfly/react-core";
import {
  MouseEvent,
  SyntheticEvent,
  useCallback,
  useEffect,
  useRef,
  useState,
  useTransition,
} from "react";
import { MessagesTableProps } from "../MessagesTable";
import { SearchParams } from "../types";
import { FromGroup } from "./FromGroup";
import { parseSearchInput } from "./parseSearchInput";
import { PartitionSelector } from "./PartitionSelector";
import { WhereSelector } from "./WhereSelector";

export function AdvancedSearch({
  filterQuery,
  filterWhere,
  filterEpoch,
  filterOffset,
  filterPartition,
  filterTimestamp,
  filterLimit,
  filterLive,
  onSearch,
  partitions,
}: Pick<
  MessagesTableProps,
  | "filterQuery"
  | "filterWhere"
  | "filterEpoch"
  | "filterOffset"
  | "filterPartition"
  | "filterTimestamp"
  | "filterLimit"
  | "filterLive"
  | "onSearch"
  | "partitions"
>) {
  const searchInputRef = useRef<HTMLInputElement>(null);
  const paneRef = useRef(null);
  const [isPanelOpen, setIsPanelOpen] = useState(false);
  const [query, setQuery] = useState(filterQuery);
  const [where, setWhere] = useState(filterWhere);
  const [partition, setPartition] = useState(filterPartition);
  const [limit, setLimit] = useState(filterLimit ?? 50);
  const [fromEpoch, setFromEpoch] = useState(filterEpoch);
  const [fromTimestamp, setFromTimestamp] = useState(filterTimestamp);
  const [fromOffset, setFromOffset] = useState(filterOffset);
  const [untilLimit, setUntilLimit] = useState(filterLimit);
  const [untilLive, setUntilLive] = useState(filterLive);
  const [shouldSubmit, setShouldSubmit] = useState(false);
  const [_, startTransition] = useTransition();

  function setLatest() {
    setFromEpoch(undefined);
    setFromTimestamp(undefined);
    setFromOffset(undefined);
  }

  function onClear() {
    setQuery(undefined);
    setLatest();
    setPartition(undefined);
    setUntilLimit(50);
    setShouldSubmit(true);
  }

  function onSubmit(
    e?: MouseEvent<HTMLButtonElement> | SyntheticEvent<HTMLButtonElement>,
  ) {
    e && e.preventDefault();
    setShouldSubmit(true);
  }

  const getParameters = useCallback((): SearchParams => {
    const from = ((): SearchParams["from"] => {
      if (fromOffset) {
        return { type: "offset", value: fromOffset };
      } else if (fromEpoch) {
        return { type: "epoch", value: fromEpoch };
      } else if (fromTimestamp) {
        return { type: "timestamp", value: fromTimestamp };
      } else {
        return { type: "latest" };
      }
    })();
    return {
      query: query
        ? {
            value: query,
            where: where ?? "everywhere",
          }
        : undefined,
      partition,
      from,
      limit,
    };
  }, [query, where, partition, limit, fromOffset, fromEpoch, fromTimestamp]);

  const getSearchInputValue = useCallback(() => {
    const parameters = getParameters();
    const { query, from, limit, partition } = parameters;
    return (() => {
      let composed: string[] = [];
      if (from) {
        if ("value" in from) {
          composed.push(`messages=${from.type}:${from.value}`);
        } else {
          composed.push(`messages=latest`);
        }
      }
      if (limit) {
        composed.push(`limit=${limit}`);
      }
      if (partition) {
        composed.push(`partition=${partition}`);
      }
      if (query) {
        composed.push(query.value);
        if (query.where !== "everywhere") {
          composed.push(`where=${query.where}`);
        }
      }
      return composed.join(" ");
    })();
  }, [getParameters]);

  const [searchInputValue, setSearchInputValue] = useState(
    getSearchInputValue(),
  );

  const doSubmit = useCallback(
    (e?: MouseEvent<HTMLButtonElement> | SyntheticEvent<HTMLButtonElement>) => {
      e && e.preventDefault();
      startTransition(() => {
        onSearch(getParameters());
      });
    },
    [getParameters, onSearch],
  );

  useEffect(() => {
    if (shouldSubmit) {
      setShouldSubmit(false);
      setIsPanelOpen(false);
      void doSubmit();
    }
  }, [doSubmit, shouldSubmit]);

  const searchInput = (
    <SearchInput
      value={searchInputValue}
      onChange={(_, v) => setSearchInputValue(v)}
      onToggleAdvancedSearch={(e) => {
        e.stopPropagation();
        setIsPanelOpen((v) => !v);
      }}
      isAdvancedSearchOpen={isPanelOpen}
      onClear={onClear}
      onSearch={(e) => {
        e.preventDefault();
        const sp = parseSearchInput({
          value: searchInputValue,
        });
        startTransition(() => {
          onSearch(sp);
        });
      }}
      ref={searchInputRef}
      id="search"
    />
  );

  const advancedForm = (
    <div ref={paneRef} role="dialog" aria-label="Search messages">
      <Panel variant="raised">
        <PanelMain>
          <PanelMainBody>
            <Form isHorizontal={true} isWidthLimited={true}>
              <FormSection title={"Filter"}>
                <Grid hasGutter={true}>
                  <GridItem>
                    <FormGroup
                      label="Has the words"
                      fieldId="has-words"
                      key="has-words"
                    >
                      <TextInput
                        type="text"
                        id="query"
                        value={query}
                        onChange={(_event, value) => setQuery(value)}
                      />
                      <FormHelperText>
                        <HelperText>
                          <HelperTextItem>
                            Enter your search terms to find matches within the
                            selected field. Wildcards and regular expressions
                            are not required or supported.
                          </HelperTextItem>
                        </HelperText>
                      </FormHelperText>
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup label={"Where"}>
                      <WhereSelector value={where} onChange={setWhere} />
                    </FormGroup>
                  </GridItem>
                </Grid>
              </FormSection>
              <FormSection title={"Parameters"}>
                <Grid hasGutter={true}>
                  <GridItem>
                    <FormGroup label={"Messages"}>
                      <FromGroup
                        offset={filterOffset}
                        epoch={filterEpoch}
                        timestamp={filterTimestamp}
                        onOffsetChange={(v) => {
                          setFromOffset(v);
                          setFromEpoch(undefined);
                          setFromTimestamp(undefined);
                        }}
                        onTimestampChange={(v) => {
                          setFromOffset(undefined);
                          setFromEpoch(undefined);
                          setFromTimestamp(v);
                        }}
                        onEpochChange={(v) => {
                          setFromOffset(undefined);
                          setFromEpoch(v);
                          setFromTimestamp(undefined);
                        }}
                        onLatest={setLatest}
                      />
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup label={"Limit"}>
                      <LimitSelector
                        value={untilLimit ?? 50}
                        onChange={setUntilLimit}
                      />
                    </FormGroup>
                  </GridItem>

                  <GridItem>
                    <FormGroup
                      label="In partition"
                      fieldId="in-partition"
                      key="in-partition"
                    >
                      <PartitionSelector
                        value={partition}
                        partitions={partitions}
                        onChange={setPartition}
                      />
                    </FormGroup>
                  </GridItem>
                </Grid>
              </FormSection>
              <ActionGroup>
                <Button
                  variant="primary"
                  type="submit"
                  onClick={(e) => onSubmit(e)}
                >
                  Search
                </Button>
                {!!onClear && (
                  <Button variant="link" type="reset" onClick={onClear}>
                    Reset
                  </Button>
                )}
              </ActionGroup>
            </Form>
          </PanelMainBody>
        </PanelMain>
      </Panel>
    </div>
  );

  // Popper is just one way to build a relationship between a toggle and a menu.
  return (
    <Popper
      trigger={searchInput}
      triggerRef={searchInputRef}
      popper={advancedForm}
      popperRef={paneRef}
      isVisible={isPanelOpen}
      enableFlip={false}
      appendTo={() => document.querySelector("body")!}
    />
  );
}
