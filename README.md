
# dw-sftp-to-s3
This is a tool to automatically download files from an sftp server to s3 with client side encryption.  The tool lists all of the files in the sftp and then determines which ones do not exist in our s3 bucket.  If there are any files that do not exist, it downloads them and depending on your user settings, decrypts them with a pgp key.  It then encrypts them with our kms key and uploads them to s3.

Currently, this tool supports connecting to sftp servers via password or a key.  It currently only supports data stored on sftp that is either pgp encrypted or completely unencrypted.  

In order to use this repo, you will need to download the kms encrypted application.conf file from our s3 bucket (dw-sftp-to-s3/configs).  

## Configuration File
The application.conf file is constructed of several job descriptions.  Inside each job is a section for sftp, encryption, and aws details:

An example of the configuration file can be found [here](https://github.com/gilt/dw-sftp-to-s3/blob/master/src/main/resources/application.example.conf)

 
### SFTP Settings with Password
| Item | Description  | Type 
|--|--|--|
|host  | The hostname of the sftp site   | String
|username  | The username to log into the sftp site  | String
|password  | The password to log into the sftp site  | String
|path  | This is the directory that the data files live in on the sftp server.  It must end in "/" if not blank  | String
|filter  | A regex filter to choose which files to extract from the above directory  | String
|type  | Either key or password.  In this example it is password.| String

### SFTP Settings with Key
| Item | Description  | Type 
|--|--|--|
|host  | The hostname of the sftp site   | String
|username  | The username to log into the sftp site  | String
|key  | This indicates the config information for the key  | Config
|keyName  | Name of the key on S3  | String
|kms.aws  | This indicates the config information for aws where the key is located  | Config
|kms.aws.bucket  | Name of the bucket where the key is located on S3  | String
|kms.aws.kms  | This is the kms key id that was used to client side encrypt the key file on S3  | String
|path  | This is the directory that the data files live in on the sftp server.  It must end in "/" if not blank  | String
|filter  | A regex filter to choose which files to extract from the above directory  | String
|type  | Either key or password.  In this example it is key.| String


### Encryption Settings with PGP Encrypted Files
| Item | Description  | Type 
|--|--|--|
|encrypted  | Whether or not the files are PGP encrypted.  In this example, this is true.   | Boolean
|publicKey  | This is the name of the public pgp key file  | String
|privateKey  | This is the name of the private pgp key file  | Config
|aws  | This indicates the config information for aws where the pgp keys are located  | Config
|aws.bucket  | Name of the bucket where the pgp keys are located on S3  | String
|aws.kms  | This is the kms key id that was used to client side encrypt the pgp key files on S3  | String
|password  | If the private key is protected with a password, put it here.  If not, leave this as a blank string.  | String

### Encryption Settings with Unencrypted Files
| Item | Description  | Type 
|--|--|--|
|encrypted  | Whether or not the files are PGP encrypted.  In this example, this is false.   | Boolean


### AWS
| Item | Description  | Type 
|--|--|--|
|bucket  | This is the bucket that the data files will land in on S3   | String
|kms  | This is the kms key id that will be used to client side encrypt the data files   | String


## Add a Job
Add a new job to the configuration files that include sftp, encryption, and aws sections and push to s3://dw-sftp-to-s3/configs.

## Running Locally
Clone the repo, and open the project in Intellij.  Build the project, and then edit the run configurations.  Set up an application configuration that points the Main class to Main.  Then set the program arguments to the name of the job you would like to run (must match the job name in the config file).  Then run this configuration.

## Building the Docker Image Locally
Instead of creating your own docker file, sbt can make your docker image for you.

To do this, run `sbt 'docker:publishLocal'`

## Cloudformation
The cloudformation step is included in deployment.  For reference, the template creates an ECS Repository, an IAM Role, and a Batch Job Definition.


## Deployment

To deploy, ensure you have a valid token from AWS, if not run
```
okta-aws-login --user <username> --aws-profile <profile (example: aws-data)> --region <region (example: us-east-1)>
```
Once you have a valid token you should run the deploy script on the project root.

To get information on the current tags, run the following commands
```
git pull --tags
git tag
```

Use [semver](https://semver.org/) to define a version and always release from up to date master
```
git tag -v [new version]
git push --tags
./deploy.sh
```

