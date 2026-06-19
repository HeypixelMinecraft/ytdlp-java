/**
 * Bridge script for ExternalJsChallengeSolver.
 * Requires yt-dlp-ejs package: set --ejs-script to yt-dlp-ejs solver entry, or install globally.
 *
 * Protocol: read JSON from stdin, write {"results":{...}} to stdout.
 */
const fs = require('fs');

async function main() {
  const input = fs.readFileSync(0, 'utf8');
  const req = JSON.parse(input);
  const results = {};

  // Placeholder: if yt-dlp-ejs is available via YTDLP_EJS_PATH env, delegate to it.
  const ejsPath = process.env.YTDLP_EJS_PATH;
  if (ejsPath) {
    try {
      const solver = require(ejsPath);
      if (req.type === 'n' && solver.solveN) {
        for (const c of req.challenges) {
          results[c] = await solver.solveN(c, req.playerUrl, req.videoId);
        }
      } else if (req.type === 'sig' && solver.solveSig) {
        for (const c of req.challenges) {
          results[c] = await solver.solveSig(c, req.playerUrl, req.videoId);
        }
      }
    } catch (e) {
      console.error('yt-dlp-ejs error:', e.message);
      process.exit(1);
    }
  } else {
    console.error('YTDLP_EJS_PATH not set; install yt-dlp-ejs and point env to solver module');
    process.exit(1);
  }

  process.stdout.write(JSON.stringify({ results }));
}

main();
