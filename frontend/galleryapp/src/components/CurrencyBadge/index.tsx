import { Badge } from "@r3/r3-tooling-design-system";
import { FC, useContext } from "react";
import { UsersContext } from "@Context/users";

interface Props {
  currencyCode: string;
}

const CurrencyBadge: FC<Props> = ({ currencyCode, children }) => {
  const { networkColours } = useContext(UsersContext);
  const currencyColor = networkColours[currencyCode];

  return (
    <Badge customColour={currencyColor} variant="gray">
      {children}
    </Badge>
  );
};

export default CurrencyBadge;
