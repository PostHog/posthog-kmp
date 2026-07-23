#!/usr/bin/env node
// node scripts/enrich-changelog.mjs
//
// Restores the changelog attribution @changesets/changelog-github used to
// provide. `pnpm version -r` writes plain summaries; this rewrites each fresh
// bullet in the just-created CHANGELOG section as
//   - [<short-sha>](<commit-url>) <summary> — Thanks @<author>!
// using the commit that introduced the consumed change intent, and stamps the
// release date on the version heading (## <version> — YYYY-MM-DD).
//
// Must run after `pnpm version -r`, in the same working tree: consumed intents
// are read from the unstaged deletions of .changeset/*.md. Author resolution
// needs `gh` with GH_TOKEN. Failures degrade to a plain entry (a warning, not
// a failed release).
//
// The release workflow pins this script's sha256 (EXPECTED_ENRICH_SCRIPT_SHA256
// in .github/workflows/release.yml); update it there when changing this file.

import { execFileSync } from 'node:child_process'
import { readFileSync, writeFileSync } from 'node:fs'

const warn = (msg) => console.warn(`enrich-changelog: ${msg}`)
const git = (...args) => execFileSync('git', args, { encoding: 'utf8' })

const repo = process.env.GITHUB_REPOSITORY ?? 'PostHog/posthog-kmp'
const version = JSON.parse(readFileSync('package.json', 'utf8')).version
const lines = readFileSync('CHANGELOG.md', 'utf8').split('\n')

const headingIdx = lines.findIndex((l) => l === `## ${version}`)
if (headingIdx === -1) {
  warn(`no "## ${version}" heading found; leaving CHANGELOG.md unchanged`)
  process.exit(0)
}
let sectionEnd = lines.length
for (let i = headingIdx + 1; i < lines.length; i++) {
  if (lines[i].startsWith('## ')) {
    sectionEnd = i
    break
  }
}

const consumedIntents = git('diff', '--name-only', '--diff-filter=D', '--', '.changeset/*.md')
  .split('\n')
  .filter(Boolean)

for (const intentPath of consumedIntents) {
  try {
    const raw = git('show', `HEAD:${intentPath}`)
    const parts = raw.match(/^---\n([\s\S]*?)\n---\n([\s\S]*)$/)
    if (!parts) continue
    const [, frontmatter, rawBody] = parts
    // "none" bumps never get a changelog bullet
    if (!/:\s*['"]?(patch|minor|major)['"]?\s*$/m.test(frontmatter)) continue
    const body = rawBody.trim()
    if (!body) continue
    const firstLine = body.split('\n')[0].trim()

    const bulletIdx = lines.findIndex(
      (l, i) => i > headingIdx && i < sectionEnd && l === `- ${firstLine}`,
    )
    if (bulletIdx === -1) {
      warn(`${intentPath}: no matching bullet for "${firstLine}"; skipping`)
      continue
    }

    const sha = git('log', '--diff-filter=A', '-n1', '--format=%H', '--', intentPath).trim()
    if (!sha) {
      warn(`${intentPath}: introducing commit not found; skipping`)
      continue
    }

    // Thank the author of the PR that introduced the intent (like
    // changelog-github); fall back to the commit author for direct pushes.
    let login = ''
    try {
      login = execFileSync(
        'gh',
        ['api', `repos/${repo}/commits/${sha}/pulls`, '--jq', '.[0].user.login // empty'],
        { encoding: 'utf8' },
      ).trim()
      if (!login) {
        login = execFileSync(
          'gh',
          ['api', `repos/${repo}/commits/${sha}`, '--jq', '.author.login // empty'],
          { encoding: 'utf8' },
        ).trim()
      }
    } catch {
      warn(`${intentPath}: could not resolve the author of ${sha}; omitting thanks`)
    }

    // The bullet block ends at its last non-blank line before the next bullet
    // or heading (multi-line summaries continue on indented lines).
    let blockEnd = bulletIdx
    for (let i = bulletIdx + 1; i < sectionEnd; i++) {
      if (lines[i].startsWith('- ') || lines[i].startsWith('#')) break
      if (lines[i].trim() !== '') blockEnd = i
    }

    lines[bulletIdx] = `- [${sha.slice(0, 7)}](https://github.com/${repo}/commit/${sha}) ${firstLine}`
    if (login) lines[blockEnd] += ` — Thanks @${login}!`
  } catch (err) {
    warn(`${intentPath}: ${err.message}; skipping`)
  }
}

const date = new Date().toISOString().slice(0, 10)
lines[headingIdx] = `## ${version} — ${date}`
writeFileSync('CHANGELOG.md', lines.join('\n'))
console.log(`enrich-changelog: enriched "## ${version} — ${date}" (${consumedIntents.length} intent(s))`)
