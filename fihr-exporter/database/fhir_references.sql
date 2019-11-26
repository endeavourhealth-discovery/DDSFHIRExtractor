-- MySQL dump 10.13  Distrib 5.7.17, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: fhir_references
-- ------------------------------------------------------
-- Server version	5.7.20-log

-- CREATE DATABASE /*!32312 IF NOT EXISTS*/ `fhir_references` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE `data_extracts`;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `references`
--

DROP TABLE IF EXISTS `references`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `references` (
  `an_id` bigint(20) DEFAULT NULL,
  `strid` varchar(255) DEFAULT NULL,
  `resource` varchar(255) DEFAULT NULL,
  `response` varchar(10) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `datesent` datetime NOT NULL,
  `json` text DEFAULT NULL,
  `patient_id` bigint(20) DEFAULT NULL,
  `type_id` tinyint(1) DEFAULT NULL,
  `runguid` varchar(50) DEFAULT NULL,
  KEY `ix_references_an_id` (`an_id`),
  KEY  `ix_references_strid` (`strid`),
  KEY  `ix_references_location` (`location`),
  KEY  `ix_references_resource` (`resource`),
  KEY  `ix_references_response` (`response`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;