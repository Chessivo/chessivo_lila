import debounce from 'common/debounce';

export type WikiTheory = (nodes: Tree.Node[]) => void;
declare const LZString: any;

export default function wikiTheory(): WikiTheory {
  const cache = new Map<string, string>();
  const show = (html: string) => {
	  if (html === "" && $('.analyse__wiki').html().includes("Chessivo")) {
		// Do nothing because we don't want to clear the content
	  } else {
		$('.analyse__wiki').html(html).toggleClass('empty', !html);
	  }
	  lichess.pubsub.emit('chat.resize');
	};

  const plyPrefix = (node: Tree.Node) => `${Math.floor((node.ply + 1) / 2)}${node.ply % 2 === 1 ? '._' : '...'}`;

  const wikiBooksUrl = 'https://en.wikibooks.org';
  const apiArgs = 'redirects&origin=*&action=query&prop=extracts&formatversion=2&format=json&exchars=1200';

  const removeEmptyParagraph = (html: string) => html.replace(/<p>(<br \/>|\s)*<\/p>/g, '');

  const removeTableHeader = (html: string) => html.replace('<h2><span id="Theory_table">Theory table</span></h2>', '');
  const removeTableExpl = (html: string) =>
    html.replace(/For explanation of theory tables see theory table and for notation see algebraic notation.?/, '');
  const removeContributing = (html: string) =>
    html.replace('When contributing to this Wikibook, please follow the Conventions for organization.', '');

  const readMore = (title: string) =>
    `<p><a target="_blank" href="${wikiBooksUrl}/wiki/${title}">Read more on WikiBooks</a></p>`;
  const checkAnalysisProgress = async (id: string) => {
    try {
      const res = await fetch(`https://chessivo.com/php/check_analysis.php?id=${id}`);
      const result = await res.text();
      return result === '1';
    } catch (err) {
      console.error(err);
      return false;
    }
  };

  let isPolling = false; // Add this line
  let lastAnalysis: string = "";

  const pollForAnalysisCompletion = async (id: string) => {
    if (isPolling) return; // Add this line
    isPolling = true; // Add this line
    while (true) {
      const ready = await checkAnalysisProgress(id);
      if (ready) {
        location.reload();
        break;
      }
      await new Promise(resolve => setTimeout(resolve, 3000));
    }
    isPolling = false; // Add this line
  };
  const transform = (html: string, title: string) =>
    removeEmptyParagraph(removeTableHeader(removeTableExpl(removeContributing(html)))) + readMore(title);
  interface FenMap {
        [fen: string]: number;
  }
    function beautifyChessAnalysis(inputText: string, lastMove: string) {
        // Split text into sentences
		inputText = inputText.replace(/\n/g, '<br>');
        let sentences = inputText.match(/[^\.!\?]+[\.!\?]+/g) || [];

        // Process sentences
        let processedText = `<h1 style="color: hsl(307, 80%, 59%)">Chessivo AI Analysis for move ${lastMove}</h1>`;

        for (let i = 0; i < sentences.length; i++) {
            let sentence = sentences[i];

            // Remove ": " text at the start of the first sentence
            if (i === 0) {
                sentence = sentence.replace(/^:\s*/, '');
            }

            // Remove sentences that begin with "In move" or contain "It is always recommended"
            if (sentence.trim().startsWith("In move") || sentence.includes("It is always recommended")
                || sentence.includes("It is important to note")) {
                continue;
            }

            // Detect chess moves and make them bold
            sentence = sentence.replace(/(\s|^)([RNBQK]?[a-h]?[1-8]?[x-]?[a-h][1-8](\=[RNBQK])?(\+|#)?|O-O-O|O-O)/g, ' <b>$2</b>');

            // Append the sentence to the processed text
            processedText += sentence + " ";

            // Check the next character after the period
            let nextCharacter = (i < sentences.length - 1) ? sentences[i + 1].trim().charAt(0) : "";

            // Add a paragraph break after some sentences for readability
            if (
                (sentence.includes("However,") || sentence.includes("After") || sentence.includes("Overall,")
                    || sentence.includes("In summary") || sentence.includes("In conclusion")) &&
                isNaN(parseInt(nextCharacter)) // Exclude cases where the character after the period is a digit
            ) {
                //processedText += "<br><br>";
            }
        }
		let voteButtonHtml = '<div class="vote" data-id="0">' +
                '<button class="upvote">👍</button>' +
                '<button class="downvote">👎</button>' +
                '<div class="message"></div>' +
                '</div>';
		processedText = processedText + '<br><br> '+voteButtonHtml;

        return processedText;
    }
  interface Analysis {
        [move: string]: any; // You may want to provide a more specific type here instead of any
  }
  return debounce(
    async (nodes: Tree.Node[]) => {
      const pathParts = nodes.slice(1).map(n => `${plyPrefix(n)}${n.san}`);
      const path = pathParts.join('/').replace(/[+!#?]/g, '') ?? '';
      const analysis = LZString.decompressFromBase64((lichess as any).payload);
	  
	  if((((lichess as any).payload === "working" || (lichess as any).payload === "" || typeof (lichess as any).payload === "undefined"))) {
          const id = window.location.href.split('/')[3]; // Assumes the URL is always in the expected format
          if(nodes.length === 1) {
		     show('<h1 style="color: hsl(307, 80%, 59%)">Chessivo AI Analysis</h1><br>AI analysis is in progress. This could take a few minutes. Please wait...');
		  }
          pollForAnalysisCompletion(id);
          return;
      }
	  
      if(typeof (lichess as any).payload != "undefined" && analysis != null && !(lichess as any).payload.includes("working") && analysis != "" && nodes.length >= 1) {
          let analysis_obj: Analysis = JSON.parse(analysis);
          let myfen = nodes[nodes.length-1].fen;
          let lichessFenMap = (lichess as any).fenmap as FenMap;
          if(myfen in lichessFenMap) {
              let mymove = lichessFenMap[myfen];
              if(""+(mymove-1) in analysis_obj) {
                  let analysis = analysis_obj[""+(mymove-1)];
                  //console.log("Looked up analysis: "+analysis);
				  let lastMove = pathParts.length > 0 ? pathParts[pathParts.length - 1].replace(/_/g, ' ') : "";
                  lastAnalysis = beautifyChessAnalysis(analysis, lastMove);
                  show(lastAnalysis);
				  $('.upvote, .downvote').on('click', function(e: Event){ // line 182
						const el = e.currentTarget as HTMLElement;
						var voteContainer = $(el).closest('.vote');
						var id = 0;
						var vote = $(el).hasClass('upvote') ? 'upvote' : 'downvote';
						var userTag = $('#user_tag');
						var username = userTag.length ? userTag.text() : 'Unknown';
						$('.upvote, .downvote', voteContainer).remove();

						// Build the annotation string
						var annotation = "User: " + username + ", Url: " + window.location.href;
						//alert("VOTE");
						
						fetch('https://chessivo.com/php/vote.php', {
							method: 'POST',
							headers: {
								'Content-Type': 'application/json'
							},
							body: JSON.stringify({vote: vote, id: id, annotation: annotation})
						})
						.then(_response => {
							var messageDiv = $('.message', voteContainer);
							messageDiv.text('Thank you for your feedback!');
							messageDiv.addClass('fade');
							setTimeout(function(){
								messageDiv.removeClass('fade');
							}, 10000);  // remove the class after 10 seconds
						});
					});
				return;
              }
          }

          //console.log("HUHU: "+JSON.stringify(analysis_obj));
      }
	  
	  if(lastAnalysis != "") {
	  
	  }


      if (pathParts.length > 30 || !path || path.length > 255 - 21) show('');
      else if (cache.has(path)) {
	     show(cache.get(path)!);
	  }
      else if (
        Array.from({ length: pathParts.length }, (_, i) => -i - 1)
          .map(i => pathParts.slice(0, i).join('/'))
          .some(sub => cache.has(sub) && !cache.get(sub)!.length)
      )
        show('');
      else {
        const title = `Chess_Opening_Theory/${path}`;
        try {
          const res = await fetch(`${wikiBooksUrl}/w/api.php?titles=${title}&${apiArgs}`);
          const saveAndShow = (html: string) => {
            cache.set(path, html);
            show(html);
          };
          if (res.ok) {
            const json = await res.json();
            const page = json.query.pages[0];
            if (page.missing) saveAndShow('');
            else if (page.invalid) show('invalid request: ' + page.invalidreason);
            else if (!page.extract) show('error: unexpected API response:<br><pre>' + JSON.stringify(page) + '</pre>');
            else saveAndShow(transform(page.extract, title));
          } else saveAndShow('');
        } catch (err) {
          show('error: ' + err);
        }
      }
    },
    500,
    true
  );
}

export function wikiClear() {
  $('.analyse__wiki').html('').toggleClass('empty', true);
}

$(document).ready(function() {
  if((((lichess as any).payload === "working" || (lichess as any).payload === "" || typeof (lichess as any).payload === "undefined"))) {
    $('.analyse__wiki').html('<h1 style="color: hsl(307, 80%, 59%)">Chessivo AI Analysis</h1><br>AI analysis is in progress. This could take a few minutes. Please wait...').toggleClass('empty', false);
	}
});
