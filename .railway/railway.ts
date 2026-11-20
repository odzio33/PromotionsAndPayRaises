import { defineRailway, github, project, service } from "railway/iac";
// This repository manages only its own resources in the environment. Other
// repositories export their own partial name.
// See https://docs.railway.com/infrastructure-as-code#multi-repo-projects
export const partial = "pay-raise-module";
export default defineRailway(() => {
  const backend = service("backend", {
    source: github("odzio33/PromotionsAndPayRaises", { branch: "main", checkSuites: false }),
    build: { buildEnvironment: "V3", builder: "DOCKERFILE", dockerfilePath: "Dockerfile" },
    healthcheck: "/actuator/health",
    healthcheckTimeout: 300,
    replicas: { "sfo": 1 },
  });

  const frontend = service("frontend", {
    source: github("odzio33/PromotionsAndPayRaises", {
      branch: "main",
      checkSuites: false,
      rootDirectory: "frontend",
    }),
    build: { buildEnvironment: "V3", builder: "DOCKERFILE", dockerfilePath: "Dockerfile" },
    replicas: { "sfo": 1 },
  });

  return project("pay-raise-module", {
    variables: { managed: false },
    resources: [backend, frontend],
  });
});
