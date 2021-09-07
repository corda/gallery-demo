import styles from "./styles.module.scss";
import ParticipantSelection from "@Components/ParticipantSelection";
import { UsersContext } from "@Context/users";
import { useContext } from "react";

function Home() {
  const { list } = useContext(UsersContext);

  return (
    <div className={styles.main}>
      <h1>Select a participant</h1>
      <ParticipantSelection participants={list} />
    </div>
  );
}

export default Home;
