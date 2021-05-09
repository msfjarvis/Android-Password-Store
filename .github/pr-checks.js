function checkChangedFiles({github, context}) {
  const result = await github.pulls.listFiles({
    owner: context.payload.repository.owner.login,
    repo: context.payload.repository.name,
    pull_number: context.payload.number,
    per_page: 100
  })
  const files = result.data.filter(file =>
    // We wanna run this if the PR workflow is modified
    (file.filename.endsWith(".yml") && !file.filename.endsWith("pull_request.yml")) ||
    // Changes in Markdown files don't need tests
    file.filename.endsWith(".md") ||
    // Changes to fastlane metadata aren't covered by tests
    file.filename.startsWith("fastlane/")
  )
  // If filtered file count and source file count is equal, it means all files
  // in this PR are skip-worthy.
  return files.length != result.data.length
}

module.exports = {
  checkChangedFiles
}
