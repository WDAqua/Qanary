smaph-erd Wrapper for Qanary
============================

In order to use this component you need to install the [smaph-erd fork](https://github.com/WDAqua/smaph-erd) and run smaph system first.

Alternatively the Dockerfile included will create a docker container running smaph and its wrapper for Qanary. Before you will start the docker container you will need to:
* obtain a key of the Bing Search API [here](https://datamarket.azure.com/dataset/bing/search)
* edit **smaph-config.xml** replacing **BING_KEY** with your [Primary Account Key](https://datamarket.azure.com/account)
