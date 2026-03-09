const hre = require("hardhat");

async function main() {
    console.log("Deploying ChainRXCommunity...");

    const DAO = await hre.ethers.getContractFactory("ChainRXCommunity");
    const dao = await DAO.deploy();

    await dao.waitForDeployment();

    console.log("ChainRXCommunity deployed to:", await dao.getAddress());
}

main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
