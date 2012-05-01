<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
	xmlns="http://www.pilgrimsofnatac.com/schemas/game.xsd" 
	xmlns:game="http://www.pilgrimsofnatac.com/schemas/game.xsd"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:xalan="http://xml.apache.org/xalan">
	
	<xsl:param name="playerId" />
	
	<xsl:template match="/">
		<xsl:comment><xsl:text>current player: </xsl:text><xsl:value-of select="$playerId" /></xsl:comment>
		<xsl:apply-templates />
	</xsl:template>
	
	
	
	<xsl:template match="*">
		<xsl:variable name="privateOf" select="@privateOf" />
		
		<xsl:choose>
			<xsl:when test="not(@privateOf)">
				<xsl:copy>
					<xsl:apply-templates select="*|@*|text()" />
				</xsl:copy>
			</xsl:when>
			<xsl:when test="xalan:evaluate(@privateOf) = $playerId">
				<xsl:copy>
					<xsl:apply-templates select="*|@*|text()" />
				</xsl:copy>
			</xsl:when>
			<xsl:otherwise>
				<xsl:comment><xsl:text>private of player: </xsl:text><xsl:value-of select="xalan:evaluate(@privateOf)" /></xsl:comment>
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>
	
	<xsl:template match="@*|text()">
		<xsl:copy />
	</xsl:template>

</xsl:stylesheet>