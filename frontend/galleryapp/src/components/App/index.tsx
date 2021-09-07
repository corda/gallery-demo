import React from "react";
import "./index.scss";
import "@r3/r3-tooling-design-system/lib/index.scss";
import SiteHeader from "@Components/SiteHeader";
import ActivityLog from "@Components/ActivityLog";
import { BrowserRouter as Router, Switch, Route } from "react-router-dom";
import Home from "@Pages/Home";
import Gallery from "@Pages/Gallery";
import Patron from "@Pages/Patron";
import { UsersProvider } from "@Context/users";

function App() {
  return (
    <div className="App">
      <UsersProvider>
        <Router>
          <SiteHeader />
          <div className="content-wrapper">
            <Switch>
              <Route exact path="/">
                <Home />
              </Route>
              <Route path="/gallery/:id">
                <Gallery />
              </Route>
              <Route path="/patron/:id">
                <Patron />
              </Route>
            </Switch>
          </div>
          <div className="footer-wrapper">
            <ActivityLog title="Global activity log" />
          </div>
        </Router>
      </UsersProvider>
    </div>
  );
}

export default App;
