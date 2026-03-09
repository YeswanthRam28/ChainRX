const hre = require("hardhat");

async function main() {
    const ChainRX = await hre.ethers.deployContract("ChainRX");

    await ChainRX.waitForDeployment();

    console.log(`ChainRX deployed to: ${ChainRX.target}`);
}

main().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
