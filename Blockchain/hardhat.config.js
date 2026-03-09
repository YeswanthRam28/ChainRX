require("@nomicfoundation/hardhat-toolbox");
require("dotenv").config();

const { PRIVATE_KEY, POLYGON_RPC_URL } = process.env;

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
    solidity: "0.8.19",
    networks: {
        amoy: {
            url: POLYGON_RPC_URL || "https://rpc-amoy.polygon.technology/",
            accounts: PRIVATE_KEY ? [`0x${PRIVATE_KEY}`] : [],
        },
        shardeum: {
            url: "https://api-mezame.shardeum.org",
            accounts: PRIVATE_KEY ? [`0x${PRIVATE_KEY}`] : [],
            chainId: 8119,
        }
    },
};
