const path = require("path");
const NodePolyfillPlugin = require("node-polyfill-webpack-plugin");

const config_frontend = {
  name: "frontend",
  entry: "./target/index.js",
  output: {
    path: path.resolve(__dirname, "resources/frontend/js/libs"),
    filename: "node-modules.js",
    clean: true,
  },

  plugins: [new NodePolyfillPlugin()],
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/i,
        loader: "babel-loader",
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|png|jpg|gif)$/i,
        type: "asset",
      },

      {
        // docs: https://webpack.js.org/configuration/module/#resolvefullyspecified
        test: /\.m?js/,
        resolve: {
          fullySpecified: false,
        },
      },

      // Add your rules for custom modules here
      // Learn more about loaders from https://webpack.js.org/loaders/
    ],
  },
};

const config_frontend_ready = {
  name: "frontend_ready",
  entry: "./target/index.js",
  output: {
    path: path.resolve(__dirname, "prod/resources/frontend/js/libs"),
    filename: "node-modules.js",
    clean: true,
  },

  plugins: [new NodePolyfillPlugin()],
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/i,
        loader: "babel-loader",
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2|png|jpg|gif)$/i,
        type: "asset",
      },

      {
        // docs: https://webpack.js.org/configuration/module/#resolvefullyspecified
        test: /\.m?js/,
        resolve: {
          fullySpecified: false,
        },
      },

      // Add your rules for custom modules here
      // Learn more about loaders from https://webpack.js.org/loaders/
    ],
  },
};

module.exports = [config_frontend, config_frontend_ready];
console.log("Webpack started");
