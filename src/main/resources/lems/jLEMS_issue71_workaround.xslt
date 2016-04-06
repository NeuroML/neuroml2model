<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

	<xsl:output method="xml" omit-xml-declaration="yes" indent="yes" version="1.0" />

	<xsl:preserve-space elements="*" />

	<xsl:template match="neuroml2">
		<neuroml2>
			<xsl:apply-templates />
		</neuroml2>
	</xsl:template>

	<xsl:template match="*">
		<xsl:copy>
			<xsl:copy-of select="@*" />
			<xsl:apply-templates />
		</xsl:copy>
	</xsl:template>

 	<xsl:template match="gate[@type]|population[@type]">
		<xsl:element name="{@type}">
			<xsl:copy-of select="@*" />
			<xsl:copy-of select="*" />
		</xsl:element>
	</xsl:template>

</xsl:stylesheet>