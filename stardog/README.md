Add your stardog installation files in a folder named resources. The resources folder should include:
* The stardog application compressed, e.g., `stardog-4.1.1.zip`
* The stardog license key, e.g., `stardog-license-key.bin`
Before building the stardog image configure `Dockerfile` to reflect the stardog version: `ENV STARDOG_VER=<your-stardog-version>`

To build the stardog image write the following command in the current folder:
`docker build -t qanary/stardog .`