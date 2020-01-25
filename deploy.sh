#!/usr/bin/env bash

set -e

LIB_VERSION=$(git describe --tags --dirty --always)
DOCKER_IMAGE=326027360148.dkr.ecr.us-east-1.amazonaws.com/dw-sftp-to-s3:${LIB_VERSION}

# setting up Cloudformation stack
BASE_ARGS="--stack-name dw-sftp-to-s3 --parameters ParameterKey=DockerImage,ParameterValue=${DOCKER_IMAGE} --template-body file://./cloudformation.yml --capabilities CAPABILITY_NAMED_IAM"

{
 aws cloudformation create-stack ${BASE_ARGS}
} ||
{
 aws cloudformation update-stack ${BASE_ARGS}
} ||
{
  echo "The CloudFormation might be in ROLLBACK_COMPLETE, you can delete the stack with: aws cloudformation delete-stack --stack-name dw-sftp-to-s3 "
}
# eval $(aws ecr get-login --region us-east-1 --no-include-email)
# you should use okta-aws-login instead ^ !

sbt docker:publish

