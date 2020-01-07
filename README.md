# Search-Engines
CMU 11642 Course Project

A text-based search engine with Lucene API to retrieve over 500,000 documents from Gov2 and Clueweb09 dataset.

- Implemented several retrieval algorithms including Exact-match algorithms Ranked/Unranked Boolean and Best-match algorithms BM25 Okapi and Indri, and tuned the parameters based on the metrics of Precision@k and Mean Average Precision(MAP).
- Used query expansion algorithms such as Sequential-Dependency Models and Relevance Feedback to reconstruct queries.
- Supported ranking diversification with PM2 and xQuAD methods and evaluated the ranking results using the metrics of P-IA@k and αNDCG@k.
- Constructed a relevance predictive model based on Cornell SVM-Rank with 18 document-dependent and document-independent features to improve the precision of ranking.

