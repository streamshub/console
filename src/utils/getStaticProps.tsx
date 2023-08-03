import { type GetStaticProps } from "next";

export const getStaticProps: GetStaticProps = async function (context) {
  return {
    props: {
      // You can get the messages from anywhere you like. The recommended
      // pattern is to put them in JSON files separated by locale and read
      // the desired one based on the `locale` received from Next.js.
      messages: (await import(`../../messages/${context.locale || "en"}.json`))
        .default as IntlMessages,
    },
  };
};
