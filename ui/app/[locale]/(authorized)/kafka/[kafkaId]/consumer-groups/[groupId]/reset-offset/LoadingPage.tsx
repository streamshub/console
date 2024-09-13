import {
  Bullseye,
  Grid,
  GridItem,
  Spinner,
} from "@/libs/patternfly/react-core";
import { useTranslations } from "next-intl";

export function LoadingPage() {
  const t = useTranslations("ConsumerGroupsTable");
  return (
    <Grid hasGutter={true}>
      <GridItem>
        <Bullseye>
          <Spinner
            size="xl"
            aria-label={t("resetting_spinner")}
            aria-valuetext={t("resetting_spinner")}
          />
        </Bullseye>
      </GridItem>
      <GridItem>
        <Bullseye>{t("reseting_consumer_group_offsets_text")}</Bullseye>
      </GridItem>
    </Grid>
  );
}
