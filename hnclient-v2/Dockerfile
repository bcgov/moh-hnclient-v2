# Base image
FROM adoptopenjdk:11-jre-hotspot

#Echo steps
RUN echo Step 1 completed

#Create application and logs folder
#we need to mount log folder with the pod
#Can not create a log folder in the image due to root user restrictions in OC4

#Copy hns-client jar from target folder
COPY target/hns-client.jar /tmp

#Setting environment variables
#We are using secrets for PHARMANET environment variables
#The YAML file containing secrets command is checked into SVN

#we moved hns-client jar to tmp folder. So setting the work dir as tmp coz
WORKDIR /tmp

EXPOSE 5555

#Start HNI-ESB
CMD ["java","-jar","hns-client.jar"]
