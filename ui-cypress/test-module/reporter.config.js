const reportsPath = process.env.REPORTS_PATH || 'cypress/results'

module.exports = {
  reporterEnabled: 'spec, mocha-junit-reporter',
  mochaJunitReporterReporterOptions: {
    mochaFile: `${reportsPath}/output.xml`
  },
}
